import { Grid } from "@mui/material";
import { HashRouter, Route, Routes } from "react-router-dom";
import "./App.css";
import { useBridgeContext } from "./BridgeContext";
import { LoadingPage } from "./LoadingPage";
import { SettingsPage } from "./SettingsPage";

export function App() {
  const { ready } = useBridgeContext();

  return (
    <HashRouter>
      <Grid container direction="column">
        <Routes>
          {!ready && <Route path="*" element={<LoadingPage />} />}

          {ready && <Route path="*" element={<SettingsPage />} />}
        </Routes>
      </Grid>
    </HashRouter>
  );
}
