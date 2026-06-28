import List from "@mui/material/List";
import { useBridgeContext } from "../BridgeContext";
import { Brightness } from "./Brightness";
import { MapUpdateInterval } from "./MapUpdateInterval";
import { Scale } from "./Scale";
// import { ScaleType } from "./ScaleType";

export const Settings = () => {
  const { settings } = useBridgeContext();

  if (!settings) {
    return null;
  }

  return (
    <List disablePadding>
      {/* Scale type option hidden: changing it has no effect because the renderer
          is not recreated with the new value on scale type change. */}
      {/* <ScaleType value={settings.scaleType} /> */}

      <Scale value={settings.scale} />

      <MapUpdateInterval value={settings.mapUpdateInterval} />

      <Brightness value={settings.brightness} />
    </List>
  );
};
