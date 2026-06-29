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

#include <algorithm>
#include <cstdint>
#include <ctime>
#include <ostream>
#include <string>
#include <vector>

#include "SDL.h"
#include "color.h"
#include "game.h" // IWYU pragma: associated
#include "game_delays.h"
#include "game_interface.h"
#include "game_mode.h"
#include "interface_gamearea.h"
#include "jni.h"
#include "localevent.h"
#include "logging.h"
#include "maps.h"
#include "maps_fileinfo.h"
#include "math_base.h"
#include "players.h"
#include "rand.h"
#include "screen.h"
#include "settings.h"
#include "system.h"
#include "world.h"

namespace
{
    uint32_t lwpLastMapUpdate = 0;
    bool forceMapUpdate = true;

    constexpr int REGION_UPDATES_PER_MAP = 10;
    int lwpRegionUpdateCount = 0;

    int lwpLastScale = -1;
    fheroes2::ResolutionInfo lwpLastResolution;

    enum class LiveWallpaperEvent : int32_t
    {
        // These values must stay in sync with the WALLPAPER_EVENT_* constants in SDLActivity.java.
        Hide,
        UpdateConfigs,
        ResizeDisplay,
    };

    bool lwpHidePending = false;

    constexpr int COMMAND_PAUSE_NOW = 0x8000 + 1;

    constexpr uint32_t EVENT_POLL_DELAY = 32;

    constexpr int TILE_WIDTH = 32;

    void lwpLog( const char * event )
    {
        VERBOSE_LOG( "LWP " << event << " | forceMapUpdate=" << forceMapUpdate << " lwpHidePending=" << lwpHidePending << " lwpLastMapUpdate=" << lwpLastMapUpdate )
    }

    void pushWallpaperEvent( LiveWallpaperEvent code )
    {
        VERBOSE_LOG( "pushWallpaperEvent code=" << static_cast<int32_t>( code ) )

        if ( SDL_WasInit( SDL_INIT_EVENTS ) == 0 ) {
            return;
        }

        SDL_Event event;
        SDL_zero( event );
        event.type = SDL_USEREVENT;
        event.user.code = static_cast<int32_t>( code );
        SDL_PushEvent( &event );
    }

    void renderMap()
    {
        Interface::GameArea const & gameArea = Interface::AdventureMap::Get().getGameArea();
        fheroes2::Display & display = fheroes2::Display::instance();

        Game::updateAdventureMapAnimationIndex();
        gameArea.Redraw( display, Interface::RedrawLevelType::LEVEL_OBJECTS | Interface::RedrawLevelType::LEVEL_HEROES );
        display.render();
    }

    int32_t randomAxisCenter( int32_t displayPx, int32_t mapTiles )
    {
        const int32_t halfScreenTiles = displayPx / TILE_WIDTH / 2;
        return Rand::Get( halfScreenTiles + 1, mapTiles - halfScreenTiles - 1 );
    }

    void randomizeGameArea()
    {
        fheroes2::Display & display = fheroes2::Display::instance();
        const int32_t x = randomAxisCenter( display.width(), World::Get().w() );
        const int32_t y = randomAxisCenter( display.height(), World::Get().h() );

        VERBOSE_LOG( "randomizeGameArea x=" << x << " y=" << y )

        Interface::AdventureMap::Get().getGameArea().SetCenter( { x, y } );

        renderMap();
    }

    void loadRandomMap()
    {
        Settings & conf = Settings::Get();

        MapsFileInfoList mapsList = Maps::getAllMapFileInfos( 1 );
        const Maps::FileInfo currentMap = conf.getCurrentMapInfo();

        if ( mapsList.size() <= 1 ) {
            VERBOSE_LOG( "LWP map load SKIPPED (only one map) file=" << currentMap.filename.c_str() )
            return;
        }

        mapsList.erase( std::remove_if( mapsList.begin(), mapsList.end(), [&currentMap]( const Maps::FileInfo & info ) { return info.filename == currentMap.filename; } ),
                        mapsList.end() );

        const uint32_t randomMapIndex = Rand::Get( 0, mapsList.size() - 1 );
        const Maps::FileInfo & nextMap = mapsList.at( randomMapIndex );

        VERBOSE_LOG( "LWP map load START file=" << nextMap.filename.c_str() )
        conf.setCurrentMapInfo( nextMap );
        conf.GetPlayers().SetStartGame();
        world.LoadMapMP2( nextMap.filename, false );
        VERBOSE_LOG( "LWP map load FINISH file=" << nextMap.filename.c_str() )

        randomizeGameArea();
    }

    bool shouldUpdateMapRegion()
    {
        uint32_t const updateInterval = Settings::Get().GetLWPMapUpdateInterval();
        uint32_t const currentTime = std::time( nullptr );
        bool const isExpired = lwpLastMapUpdate <= currentTime - updateInterval;

        VERBOSE_LOG( "ShouldUpdateMapRegion" << " interval:" << updateInterval << " current: " << currentTime << " last update: " << lwpLastMapUpdate )

        return isExpired;
    }

    void randomizeVisibleMapPart()
    {
        if ( !shouldUpdateMapRegion() ) {
            return;
        }

        if ( lwpRegionUpdateCount >= REGION_UPDATES_PER_MAP ) {
            loadRandomMap();
            lwpRegionUpdateCount = 0;
        }
        else {
            randomizeGameArea();
        }

        ++lwpRegionUpdateCount;
        lwpLastMapUpdate = std::time( nullptr );
    }

    void updateBrightness()
    {
        int brightness = Settings::Get().GetLWPBrightness();
        int brightnessAlpha = ( 100 - brightness ) * 255 / 100;
        VERBOSE_LOG( "updateBrightness value: " << brightness << " alpha: " << brightnessAlpha )
        fheroes2::engine().setBrightness( brightnessAlpha );
    }

    void readConfigFile()
    {
        VERBOSE_LOG( "readConfigFile" )
        const std::string configurationFileName( Settings::configFileName );
        const std::string confFile = Settings::GetLastFile( "", configurationFileName );

        if ( System::IsFile( confFile ) ) {
            Settings::Get().Read( confFile );
        }
    }

    void resizeDisplay()
    {
        fheroes2::Display & display = fheroes2::Display::instance();
        const int scale = Settings::Get().GetLWPScale();
        const fheroes2::ResolutionInfo resolution = display.getScaledScreenSize( scale );

        if ( scale == lwpLastScale && resolution == lwpLastResolution ) {
            VERBOSE_LOG( "resizeDisplay skipped (scale " << scale << " unchanged)" )
            return;
        }
        lwpLastScale = scale;
        lwpLastResolution = resolution;

        VERBOSE_LOG( "resizeDisplay scale: " << scale )

        Interface::GameArea & gameArea = Interface::AdventureMap::Get().getGameArea();
        const fheroes2::Point center = gameArea.getCurrentCenterInPixels();

        display.setResolution( resolution );
        gameArea.generate( { display.width(), display.height() }, true );

        gameArea.SetCenterInPixels( center );
    }

    void rereadAndApplyConfigs()
    {
        readConfigFile();
        resizeDisplay();
        updateBrightness();
    }

    void migrateDeprecatedSettings()
    {
        Settings & conf = Settings::Get();

        if ( conf.GetLWPScale() == 0 ) {
            VERBOSE_LOG( "migrating deprecated DPI scale to " << 5 )
            conf.SetLWPScale( 5 );
            conf.Save( Settings::configFileName );
        }
    }

    void forceUpdates()
    {
        if ( forceMapUpdate ) {
            randomizeVisibleMapPart();
            forceMapUpdate = false;
        }
    }

    void handleKeyUp( SDL_Keysym keysym )
    {
        Settings & conf = Settings::Get();
        Interface::GameArea & gameArea = Interface::AdventureMap::Get().getGameArea();

        int const offsetMultiplier = keysym.mod & KMOD_SHIFT ? 10 : 1;
        int const offset = TILE_WIDTH * offsetMultiplier;

        switch ( keysym.scancode ) {
        case SDL_SCANCODE_SPACE:
            forceMapUpdate = true;
            break;
        case SDL_SCANCODE_1:
        case SDL_SCANCODE_2:
        case SDL_SCANCODE_3:
        case SDL_SCANCODE_4:
        case SDL_SCANCODE_5:
            conf.SetLWPScale( keysym.scancode - SDL_SCANCODE_1 + 1 );
            break;
        case SDL_SCANCODE_0:
            conf.SetLWPScale( 0 );
            break;
        case SDL_SCANCODE_UP:
            gameArea.ShiftCenter( { 0, -offset } );
            return;
        case SDL_SCANCODE_DOWN:
            gameArea.ShiftCenter( { 0, offset } );
            return;
        case SDL_SCANCODE_LEFT:
            gameArea.ShiftCenter( { -offset, 0 } );
            return;
        case SDL_SCANCODE_RIGHT:
            gameArea.ShiftCenter( { offset, 0 } );
            return;
        case SDL_SCANCODE_ESCAPE:
            exit( 0 );
        default:
            break;
        }

        conf.Save( Settings::configFileName );
        rereadAndApplyConfigs();
    }

    bool handleSDLEvents()
    {
        SDL_Event event;

        while ( SDL_PollEvent( &event ) ) {
            switch ( event.type ) {
            case SDL_RENDER_TARGETS_RESET:
                VERBOSE_LOG( "SDL_RENDER_TARGETS_RESET" )
                fheroes2::Display::instance().render();
                break;
            case SDL_RENDER_DEVICE_RESET:
                VERBOSE_LOG( "SDL_RENDER_DEVICE_RESET" )
                LocalEvent::onRenderDeviceResetEvent();
                fheroes2::Display::instance().render();
                break;
            case SDL_USEREVENT:
                switch ( static_cast<LiveWallpaperEvent>( event.user.code ) ) {
                case LiveWallpaperEvent::Hide:
                    lwpHidePending = true;
                    break;
                case LiveWallpaperEvent::UpdateConfigs:
                    rereadAndApplyConfigs();
                    break;
                case LiveWallpaperEvent::ResizeDisplay:
                    resizeDisplay();
                    break;
                default:
                    break;
                }
                lwpLog( "handled wallpaper event" );
                break;
            case SDL_KEYUP:
                if ( event.key.keysym.scancode == SDL_SCANCODE_ESCAPE ) {
                    return true;
                }
                handleKeyUp( event.key.keysym );
                break;
            default:
                break;
            }
        }

        return false;
    }

    fheroes2::GameMode renderWallpaper()
    {
        while ( true ) {
            const bool isEscapePressed = handleSDLEvents();
            if ( isEscapePressed ) {
                return fheroes2::GameMode::QUIT_GAME;
            }

            if ( lwpHidePending ) {
                lwpHidePending = false;
                forceMapUpdate = true;
                lwpLog( "hidden: loading map + rendering frame" );
                forceUpdates();
                renderMap();
                SDL_AndroidSendMessage( COMMAND_PAUSE_NOW, 0 );
                lwpLog( "hidden: frame posted, sent COMMAND_PAUSE_NOW" );
                continue;
            }

            forceUpdates();

            if ( Game::validateAnimationDelay( Game::DelayType::MAPS_DELAY ) ) {
                renderMap();
            }
            SDL_Delay( EVENT_POLL_DELAY );
        }
    }

    void overrideConfiguration()
    {
        Settings & conf = Settings::Get();
        conf.SetGameType( Game::TYPE_STANDARD );
        conf.SetCurrentColor( PlayerColor::NONE );
        conf.setVSync( true );
        conf.setSystemInfo( false );
        conf.setHideInterface( true );
        conf.SetShowControlPanel( false );
    }
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_pushWallpaperEvent( [[maybe_unused]] JNIEnv * env, [[maybe_unused]] jclass cls, jint code )
{
    pushWallpaperEvent( static_cast<LiveWallpaperEvent>( code ) );
}

fheroes2::GameMode Game::Wallpaper()
{
    readConfigFile();
    migrateDeprecatedSettings();
    rereadAndApplyConfigs();
    overrideConfiguration();
    loadRandomMap();

    return renderWallpaper();
}
