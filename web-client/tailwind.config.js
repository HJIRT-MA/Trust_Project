/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f8f3ea',
          100: '#efe2c9',
          200: '#e2c99b',
          300: '#d1ac6c',
          400: '#c0954f',
          500: '#ab7f3f',
          600: '#8f6a34',
          700: '#71542a',
          800: '#573f21',
          900: '#3c2b17',
          950: '#241a0d',
        },
        surface: {
          DEFAULT: '#0b0a09',
          raised: '#141210',
        },
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        display: ['Fraunces', 'ui-serif', 'Georgia', 'serif'],
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
    },
  },
  plugins: [],
}
