import "@fontsource/roboto/300.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";
import CssBaseline from "@mui/material/CssBaseline";
import { createTheme, ThemeProvider } from "@mui/material/styles";
import React from "react";
import ReactDOM from "react-dom/client";
import { App } from "./App";
import { BridgeContextProvider } from "./BridgeContext";
import "./index.css";

document.addEventListener("DOMContentLoaded", () => {
  const root = ReactDOM.createRoot(
    document.getElementById("root") as HTMLElement
  );
  const theme = createTheme({
    typography: {
      allVariants: {
        userSelect: "none",
      },
    },
  });

  root.render(
    <React.StrictMode>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <BridgeContextProvider>
          <App />
        </BridgeContextProvider>
      </ThemeProvider>
    </React.StrictMode>
  );
});
