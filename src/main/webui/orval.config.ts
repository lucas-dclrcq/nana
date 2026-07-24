import { defineConfig } from 'orval'

export default defineConfig({
  nana: {
    input: './openapi/openapi.json',
    output: {
      target: './src/api/generated/nana.ts',
      client: 'vue-query',
      clean: true,
      baseUrl: '',
      override: {
        mutator: {
          path: './src/api/http.ts',
          name: 'http',
        },
      },
    },
  },
})
