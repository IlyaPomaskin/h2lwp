import { useBridgeContext } from "../BridgeContext";
import { WallpaperScale } from "../global";
import { SimpleListMenu } from "./SimpleListMenu";

type Props = {
  value: WallpaperScale;
};

const valueToText: Record<WallpaperScale, string> = {
  0: "DPI",
  1: "x1",
  2: "x2",
  3: "x3",
  4: "x4",
  5: "x5",
};

const items = Object.entries(valueToText).map(([value, title]) => ({
  value: Number(value) as WallpaperScale,
  title,
}));

export const Scale: React.FC<Props> = ({ value }) => {
  const { androidInterface } = useBridgeContext();

  return (
    <SimpleListMenu
      label="Scale"
      value={value}
      items={items}
      onChange={(value) => androidInterface?.setScale(value)}
    />
  );
};
