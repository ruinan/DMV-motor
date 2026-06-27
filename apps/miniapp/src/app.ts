import { PropsWithChildren } from 'react'
import './app.scss'

// Root component. Taro renders the active page as children; we keep the shell
// minimal and let each page drive auth/navigation.
function App({ children }: PropsWithChildren<any>) {
  return children
}

export default App
