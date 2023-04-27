/***************************************************************************
 *   fheroes2: https://github.com/ihhub/fheroes2                           *
 *   Copyright (C) 2019 - 2023                                             *
 *                                                                         *
 *   Free Heroes2 Engine: http://sourceforge.net/projects/fheroes2         *
 *   Copyright (C) 2009 by Andrey Afletdinov <fheroes2@gmail.com>          *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

#include "game.h"

#include <algorithm>
#include <cassert>
#include <cstddef>
#include <cstdint>
#include <ostream>
#include <string>
#include <type_traits>
#include <utility>
#include <vector>

#include "agg_image.h"
#include "ai.h"
#include "army.h"
#include "audio.h"
#include "audio_manager.h"
#include "battle_only.h"
#include "castle.h"
#include "color.h"
#include "cursor.h"
#include "dialog.h"
#include "direction.h"
#include "game_delays.h"
#include "game_hotkeys.h"
#include "game_interface.h"
#include "game_io.h"
#include "game_mode.h"
#include "game_over.h"
#include "heroes.h"
#include "icn.h"
#include "image.h"
#include "interface_buttons.h"
#include "interface_cpanel.h"
#include "interface_gamearea.h"
#include "interface_icons.h"
#include "interface_radar.h"
#include "interface_status.h"
#include "kingdom.h"
#include "localevent.h"
#include "logging.h"
#include "m82.h"
#include "maps.h"
#include "maps_tiles.h"
#include "maps_tiles_helper.h"
#include "math_base.h"
#include "monster.h"
#include "mp2.h"
#include "mus.h"
#include "players.h"
#include "rand.h"
#include "resource.h"
#include "route.h"
#include "screen.h"
#include "settings.h"
#include "system.h"
#include "tools.h"
#include "translations.h"
#include "ui_dialog.h"
#include "ui_text.h"
#include "ui_tool.h"
#include "week.h"
#include "world.h"
#include "SDL_timer.h"

fheroes2::GameMode Game::Wallpaper() {
    AI::Get().Reset();

    return Interface::Basic::Get().Wallpaper();
}

fheroes2::GameMode Interface::Basic::Wallpaper() {
    Reset();

    Settings &conf = Settings::Get();
    conf.setSystemInfo(false);
    conf.SetCurrentColor(-1);
    conf.setHideInterface(true);
    conf.SetShowControlPanel(false);

    fheroes2::Display &display = fheroes2::Display::instance();
    LocalEvent &le = LocalEvent::Get();

    while (true) {
        if (le.HandleEvents(false, false) && le.KeyPress()) {
            VERBOSE_LOG("Keypress")
            // FIXME add correct hotkey
            if (le.KeyValue() == fheroes2::Key::KEY_SPACE) {
                VERBOSE_LOG("Space pressed")
                OnVisibilityChanged();
            }
        }

        Uint64 start = SDL_GetPerformanceCounter();

        Redraw(REDRAW_GAMEAREA);
        display.render();

        if (Game::validateAnimationDelay(Game::MAPS_DELAY)) {
            Game::updateAdventureMapAnimationIndex();
            gameArea.SetRedraw();
        }

        Uint64 end = SDL_GetPerformanceCounter();
        float elapsedMS = (end - start) / (float) SDL_GetPerformanceFrequency() * 1000.0f;
        SDL_Delay(static_cast<Uint32>(floor(180.0f - elapsedMS)));
    }

    return fheroes2::GameMode::END_TURN;
}

void Interface::Basic::RandomizeGameAreaPoint() {
    fheroes2::ResolutionInfo resolutionInfo = fheroes2::Display::instance().getScaledScreenSize(
            Settings::Get().GetLWPScale()
    );
    int32_t mapWidth = World::Get().w();
    int32_t mapHeight = World::Get().h();

    int32_t screenHeight = static_cast<int32_t>(floor(resolutionInfo.screenHeight / TILEWIDTH));
    int32_t screenWidth = static_cast<int32_t>(floor(resolutionInfo.screenWidth / TILEWIDTH));

    int32_t halfHeight = static_cast<int32_t>(floor(screenHeight / 2));
    int32_t halfWidth = static_cast<int32_t>(floor(screenWidth / 2));

    int32_t widthFrom = halfWidth + 1;
    int32_t widthTo = mapWidth - halfWidth - 1;
    int32_t x = Rand::Get(widthFrom, widthTo);

    int32_t heightFrom = halfHeight + 1;
    int32_t heightTo = mapHeight - halfHeight - 1;
    int32_t y = Rand::Get(heightFrom, heightTo);

    VERBOSE_LOG("Map w: " << mapWidth << " h: " << mapHeight)
    VERBOSE_LOG("Screen w: " << screenWidth << " h: " << screenHeight)
    VERBOSE_LOG("Half screen w: " << halfWidth << " h: " << halfHeight)
    VERBOSE_LOG("Width from: " << widthFrom << " to: " << widthTo)
    VERBOSE_LOG("Height from: " << heightFrom << " to: " << heightTo)
    VERBOSE_LOG("Next point x: " << x << " y: " << y)

    Interface::Basic::Get().GetGameArea().SetCenter({x, y});
}

bool Interface::Basic::ShouldUpdateMapRegion() {
    uint32_t updateInterval = Settings::Get().GetLWPMapUpdateInterval();
    uint32_t currentTime = std::time(nullptr);
    bool isFirstRun = lwpLastMapUpdate == 0;
    bool isExpired = lwpLastMapUpdate < currentTime - updateInterval;

    VERBOSE_LOG(
            "ShouldUpdateMapRegion"
                    << " interval:" << updateInterval
                    << " current: " << currentTime
                    << " last update: " << lwpLastMapUpdate
    )

    if (isFirstRun || isExpired) {
        lwpLastMapUpdate = currentTime;
        return true;
    }

    return false;
}

void RereadConfigs() {
    const std::string configurationFileName(Settings::configFileName);
    const std::string confFile = Settings::GetLastFile("", configurationFileName);

    if (System::IsFile(confFile)) {
        Settings::Get().Read(confFile);
    }
}

void Interface::Basic::OnVisibilityChanged() {
    RereadConfigs();

    if (ShouldUpdateMapRegion()) {
        RandomizeGameAreaPoint();
    }
}
