import '@testing-library/jest-dom/vitest'

import { configure } from '@testing-library/react'

// FileReader uses real asynchronous I/O; keep assertions within Vitest's test deadline.
configure({ asyncUtilTimeout: 3000 })
