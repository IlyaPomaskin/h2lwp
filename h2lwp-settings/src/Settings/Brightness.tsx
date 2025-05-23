import { Box, Grid, ListItem, Slider, Typography } from "@mui/material";
import { useEffect, useState } from "react";
import { useBridgeContext } from "../BridgeContext";

type Props = {
  value: number;
};

export const Brightness: React.FC<Props> = ({ value }) => {
  const { androidInterface } = useBridgeContext();

  const [localValue, setLocalValue] = useState(value);
  useEffect(() => {
    setLocalValue(value);
  }, [value]);

  return (
    <ListItem>
      <Grid container direction="column" width="100%">
        <Typography gutterBottom>Brightness</Typography>

        <Box marginX={2}>
          <Slider
            aria-label="Brightness"
            min={0}
            max={100}
            step={1}
            valueLabelDisplay="auto"
            valueLabelFormat={(value) => (value || 0) + "%"}
            value={localValue}
            onChange={(_, value) => {
              setLocalValue(Number(value));
              androidInterface?.setBrightness(Number(value));
            }}
          />
        </Box>
      </Grid>
    </ListItem>
  );
};
