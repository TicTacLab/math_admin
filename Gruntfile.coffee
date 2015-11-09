module.exports = (grunt) ->
  grunt.initConfig
    clean:
      public: ['resources/public/js']

    coffee:
      options:
        join: true

      public:
        expand: true
        cwd: 'front-src/'
        src: ['**/*.coffee']
        dest: 'resources/public/js/'
        ext: '.js'

    watch:
      coffee:
        files: ['front-src/**/*.coffee']
        tasks: ['coffee']


  grunt.loadNpmTasks 'grunt-contrib-clean'
  grunt.loadNpmTasks 'grunt-contrib-coffee'
  grunt.loadNpmTasks 'grunt-contrib-watch'

  grunt.registerTask 'default', ['build', 'watch']
  grunt.registerTask 'build', ['clean', 'coffee']
