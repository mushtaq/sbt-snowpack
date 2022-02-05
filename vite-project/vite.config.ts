import {defineConfig, UserConfig} from 'vite'

export default defineConfig(({command, mode}): UserConfig =>{
    // const scalaJsMode = (command === 'serve') ? "fastopt" : "fullopt"
    // const scalaJsOutput = `/target/scala-2.13/vite-project-${scalaJsMode}/main.js`
    //
    // if (command === 'serve') {
    //     return {
    //         "root": "/target/scala-2.13"
    //     }
    // } else {
    //     return {
    //         "root": "/target/scala-2.13"
    //         // build specific config
    //     }
    // }
    // ...

    return {
        // "root": "/target/scala-2.13"
    }
})
