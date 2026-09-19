/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/main/resources/templates/**/*.html',
    './src/main/resources/static/js/**/*.js',
  ],
  // Tắt preflight vì vẫn giữ Bootstrap JS bundle (modal/carousel/offcanvas/dropdown)
  // cho các component tương tác - preflight của Tailwind sẽ reset CSS và có thể
  // xung đột với style mà Bootstrap JS cần (xem CHANGES_FRONTEND_REDESIGN.md).
  corePlugins: {
    preflight: false,
  },
  theme: {
    extend: {
      colors: {
        ivory: '#FAFAF8',
        charcoal: {
          DEFAULT: '#1A1A1A',
          soft: '#2B2B2B',
          muted: '#6B6B65',
        },
        gold: {
          light: '#D9C08F',
          DEFAULT: '#B08D57',
          dark: '#8F6F41',
        },
        line: '#E5E5E0',
        sale: '#8B2635',
      },
      fontFamily: {
        heading: ['"Playfair Display"', 'serif'],
        body: ['"Inter"', 'sans-serif'],
      },
      letterSpacing: {
        wide2: '0.08em',
      },
    },
  },
  plugins: [],
}

