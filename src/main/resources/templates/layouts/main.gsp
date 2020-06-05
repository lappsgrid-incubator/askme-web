html {
    head {
        title(title)
<<<<<<< HEAD
        link rel:'stylesheet', type:'text/css', href:'css/main.css'
        script(src:'js/jquery.min.js', '')
        //script(src:'/js/main.js', '')
        if (include) {
            include.split(',').each {
                script(src:it, '')
            }
        }
        if (javascript) {
            script(javascript)
        }
        if (css) {
            style(css)
        }
        if (stylesheet) {
            if (stylesheet instanceof String || stylesheet instanceof GString) {
                link(rel:'stylesheet', type:'text/css', href:stylesheet, '')
            }
            else if (stylesheet instanceof Collection) {
                stylesheet.each { sheet ->
                    link(rel:'stylesheet', type:'text/css', href:sheet, '')
                }
            }
        }
    }
    body {
        div(class:'header') {
            h1 'The Language Applications Grid'
            h2 'Ask Me (almost) Anything'
            if (version) {
                p class:'copyright', "version $version"
            }
        }

        div(class:'content') {
            content()

            div(class:'copyright') {
                p 'Copyright &copy; 2020 The Language Applications Grid'
            }
        }
=======
//        link rel:'stylesheet', type:'text/css', href:'css/main.css'
//        script(src:'js/jquery.min.js', '')
    }
    body {
        h1 'The Language Applications Grid'
        h2 'Ask Me (almost) Anything'

        div {
            content()
        }

        p 'Copyright 2019 The Language Applications Grid'
>>>>>>> templates
    }
}
