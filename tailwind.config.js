/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,js,cljs}", "./portfolio/src/**/*.{html,js,cljs}"
  ],
  theme: {
    extend: {
      fontSize: {
        '2xs': ['10px', '14px'],
      },
      transitionProperty: {
        'fill': 'fill',
      },
    },
  },
  plugins: [],
}
