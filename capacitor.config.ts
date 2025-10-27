import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'studio.repath.app',
  appName: 'Repath Studio',
  webDir: 'resources/public',
  plugins: {
    SplashScreen: {
      launchAutoHide: false,
      backgroundColor: "#DD286C",
    },
  }
};

export default config;
