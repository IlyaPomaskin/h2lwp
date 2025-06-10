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
#include "SDL_system.h"
#include "SDL_thread.h"
#include "jni.h"
#include "SDL_events.h"

uint32_t lwpLastMapUpdate = 0;
bool forceMapUpdate = true;
bool forceConfigUpdate = true;
bool forceUpdateOrientation = true;
bool isVisible = false;

void renderMap();

constexpr int TILE_WIDTH = 32;

void loadRandomMap() {
    Settings &conf = Settings::Get();

    const MapsFileInfoList mapsList = Maps::getAllMapFileInfos(true, 0);
    const uint32_t randomMapIndex = Rand::Get(0, mapsList.size() - 1);
    const Maps::FileInfo &nextMap = mapsList.at(randomMapIndex);
    const Maps::FileInfo currentMap = conf.getCurrentMapInfo();

    if (currentMap.filename.empty() || currentMap.filename != nextMap.filename) {
        VERBOSE_LOG("map changed to file: " << nextMap.filename.c_str())
        conf.setCurrentMapInfo(nextMap);
        conf.GetPlayers().SetStartGame();
        world.LoadMapMP2(nextMap.filename, false);
    }
}

bool shouldUpdateMapRegion() {
    uint32_t const updateInterval = Settings::Get().GetLWPMapUpdateInterval();
    uint32_t const currentTime = std::time(nullptr);
    bool const isExpired = lwpLastMapUpdate <= currentTime - updateInterval;

    VERBOSE_LOG(
            "ShouldUpdateMapRegion"
                    << " interval:" << updateInterval
                    << " current: " << currentTime
                    << " last update: " << lwpLastMapUpdate
    )

    return isExpired;
}

void randomizeGameAreaPoint() {
    if (!shouldUpdateMapRegion()) {
        return;
    }

    if (lwpLastMapUpdate != 0) {
        loadRandomMap();
    }

    lwpLastMapUpdate = std::time(nullptr);

    fheroes2::Display &display = fheroes2::Display::instance();
    int32_t displayWidth = display.width();
    int32_t displayHeight = display.height();

    int32_t mapWidth = World::Get().w();
    int32_t mapHeight = World::Get().h();

    int32_t screenHeight = static_cast<int32_t>(floor(displayHeight / TILE_WIDTH));
    int32_t screenWidth = static_cast<int32_t>(floor(displayWidth / TILE_WIDTH));

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

    Interface::AdventureMap::Get().getGameArea().SetCenter({x, y});

    renderMap();
}

void updateBrightness() {
    int brightness = Settings::Get().GetLWPBrightness();
    int brightnessAlpha = static_cast<int>(floor((100 - brightness) * 255 / 100));
    VERBOSE_LOG("updateBrightness value: " << brightness << " alpha: " << brightnessAlpha)
    fheroes2::engine().setBrightness(brightnessAlpha);
}

void readConfigFile() {
    VERBOSE_LOG("readConfigFile")
    const std::string configurationFileName(Settings::configFileName);
    const std::string confFile = Settings::GetLastFile("", configurationFileName);

    if (System::IsFile(confFile)) {
        Settings::Get().Read(confFile);
    }
}

void resizeDisplay() {
    fheroes2::Display &display = fheroes2::Display::instance();
    int const scale = Settings::Get().GetLWPScale();
    VERBOSE_LOG("resizeDisplay scale: " << scale)

    display.setResolution(display.getScaledScreenSize(scale));

    Interface::AdventureMap::Get().getGameArea().generate(
            {display.width(), display.height()},
            true
    );
}

void renderMap() {
    Interface::GameArea const &gameArea = Interface::AdventureMap::Get().getGameArea();
    fheroes2::Display &display = fheroes2::Display::instance();

    Game::updateAdventureMapAnimationIndex();
    gameArea.Redraw(
            display,
            Interface::RedrawLevelType::LEVEL_OBJECTS |
            Interface::RedrawLevelType::LEVEL_HEROES);
    display.render();
}

void rereadAndApplyConfigs() {
    readConfigFile();
    resizeDisplay();
    updateBrightness();
}

void forceUpdates() {
    if (forceConfigUpdate) {
        rereadAndApplyConfigs();
        forceConfigUpdate = false;
    }

    if (forceMapUpdate) {
        randomizeGameAreaPoint();
        renderMap();
        forceMapUpdate = false;
    }

    if (forceUpdateOrientation) {
        resizeDisplay();
        forceUpdateOrientation = false;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_SDLActivity_nativeUpdateOrientation([[maybe_unused]] JNIEnv *env,
                                                        [[maybe_unused]] jclass cls) {
    VERBOSE_LOG("nativeUpdateOrientation")
    forceUpdateOrientation = true;
}


extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_SDLActivity_nativeUpdateVisibleMapRegion([[maybe_unused]] JNIEnv *env,
                                                             [[maybe_unused]] jclass cls) {
    VERBOSE_LOG("nativeUpdateVisibleMapRegion")
    forceMapUpdate = true;
}

extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_SDLActivity_nativeUpdateConfigs([[maybe_unused]] JNIEnv *env,
                                                    [[maybe_unused]] jclass cls) {
    VERBOSE_LOG("nativeUpdateConfigs")
    forceConfigUpdate = true;
}

extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_SDLActivity_nativeOnVisibilityChange([[maybe_unused]] JNIEnv *env,
                                                         [[maybe_unused]] jclass cls,
                                                         jboolean nextIsVisible) {
    VERBOSE_LOG("nativeOnVisibilityChange " << nextIsVisible)
    isVisible = nextIsVisible;
}

void handleKeyUp(SDL_Keysym keysym) {
    Settings &conf = Settings::Get();

    int const offsetMultiplier = keysym.mod & KMOD_SHIFT ? 10 : 1;
    int const offset = TILE_WIDTH * offsetMultiplier;

    switch (keysym.scancode) {
        case SDL_SCANCODE_SPACE: {
            forceMapUpdate = true;
            break;
        }
        case SDL_SCANCODE_1: {
            conf.SetLWPScale(1);
            break;
        }
        case SDL_SCANCODE_2: {
            conf.SetLWPScale(2);
            break;
        }
        case SDL_SCANCODE_3: {
            conf.SetLWPScale(3);
            break;
        }
        case SDL_SCANCODE_4: {
            conf.SetLWPScale(4);
            break;
        }
        case SDL_SCANCODE_5: {
            conf.SetLWPScale(5);
            break;
        }
        case SDL_SCANCODE_0: {
            conf.SetLWPScale(0);
            break;
        }

        case SDL_SCANCODE_UP: {
            Interface::AdventureMap::Get().getGameArea().ShiftCenter(fheroes2::Point(0, -offset));
            return;
        }
        case SDL_SCANCODE_DOWN: {
            Interface::AdventureMap::Get().getGameArea().ShiftCenter(fheroes2::Point(0, offset));
            return;
        }
        case SDL_SCANCODE_LEFT: {
            Interface::AdventureMap::Get().getGameArea().ShiftCenter(fheroes2::Point(-offset, 0));
            return;
        }
        case SDL_SCANCODE_RIGHT: {
            Interface::AdventureMap::Get().getGameArea().ShiftCenter(fheroes2::Point(offset, 0));
            return;
        }

        case SDL_SCANCODE_ESCAPE: {
            exit(0);
        }

        default: {
        }
    }

    conf.Save(Settings::configFileName);
    rereadAndApplyConfigs();
}

bool handleSDLEvents() {
    SDL_Event event;

    while (SDL_PollEvent(&event)) {
        switch (event.type) {
            case SDL_RENDER_TARGETS_RESET: {
                VERBOSE_LOG("SDL_RENDER_TARGETS_RESET")
                fheroes2::Display::instance().render();
                break;
            }
            case SDL_APP_WILLENTERBACKGROUND: {
                VERBOSE_LOG("SDL_APP_WILLENTERBACKGROUND")
                break;
            }
            case SDL_APP_DIDENTERBACKGROUND: {
                VERBOSE_LOG("SDL_APP_DIDENTERBACKGROUND")
                break;
            }
            case SDL_APP_WILLENTERFOREGROUND: {
                VERBOSE_LOG("SDL_APP_WILLENTERFOREGROUND")
                break;
            }
            case SDL_APP_DIDENTERFOREGROUND: {
                VERBOSE_LOG("SDL_APP_DIDENTERFOREGROUND")
                break;
            }
            case SDL_RENDER_DEVICE_RESET: {
                VERBOSE_LOG("SDL_RENDER_DEVICE_RESET")
                LocalEvent::onRenderDeviceResetEvent();
                fheroes2::Display::instance().render();
                break;
            }
            case SDL_DISPLAYEVENT: {
                VERBOSE_LOG("SDL_DISPLAYEVENT")
                break;
            }
            case SDL_SYSWMEVENT: {
                VERBOSE_LOG("SDL_SYSWMEVENT")
                break;
            }

            case SDL_KEYUP: {
                if (event.key.keysym.scancode == SDL_SCANCODE_ESCAPE) {
                    return true;
                }

                handleKeyUp(event.key.keysym);
            }

            default:
                break;
        }
    }

    return false;
}

fheroes2::GameMode renderWallpaper() {
    while (true) {
        if (!isVisible) {
            SDL_Delay(100);
            continue;
        }

        forceUpdates();

        const bool isEscapePressed = handleSDLEvents();
        if (isEscapePressed) {
            return fheroes2::GameMode::QUIT_GAME;
        }

        if (Game::validateAnimationDelay(Game::MAPS_DELAY)) {
            renderMap();
        } else {
            SDL_Delay(Game::getAnimationDelayValue(Game::MAPS_DELAY));
        }
    }
}

void overrideConfiguration() {
    Settings &conf = Settings::Get();
    conf.SetGameType(Game::TYPE_STANDARD);
    conf.SetCurrentColor(PlayerColor::NONE);
    conf.setVSync(true);
    conf.setSystemInfo(false);
    conf.setHideInterface(true);
    conf.SetShowControlPanel(false);
}

fheroes2::GameMode Game::Wallpaper() {
    rereadAndApplyConfigs();
    overrideConfiguration();
    loadRandomMap();

    return renderWallpaper();
}
