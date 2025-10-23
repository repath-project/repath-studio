import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'studio.repath.app',
  appName: 'Repath Studio',
  webDir: 'resources/public',
  plugins: {
    SplashScreen: {
      launchShowDuration: 2500,
      launchAutoHide: true,
      launchFadeOutDuration: 250,
      backgroundColor: "#DD286C",
    },
  }
};

export default config;
