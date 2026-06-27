export default {
  mini: {},
  h5: {
    /** Drop console in production. */
    terser: { enable: true, config: { compress: { drop_console: true } } }
  }
}
