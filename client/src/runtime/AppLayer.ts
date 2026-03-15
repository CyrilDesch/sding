import { Layer } from 'effect'
import { AuthServiceLive } from '../services/AuthService'
import { ChatServiceLive } from '../services/ChatService'
import { RouterServiceLive } from '../services/RouterService'
import { SseServiceLive } from '../services/SseService'
import { ToastServiceLive } from '../services/ToastService'
import { UserServiceLive } from '../services/UserService'

export const AppLayer = Layer.mergeAll(
  AuthServiceLive,
  RouterServiceLive,
  ChatServiceLive,
  UserServiceLive,
  SseServiceLive,
  ToastServiceLive
)

// Derive the services type from the layer so ManagedRuntime is correctly typed.
export type AppServices = Layer.Layer.Success<typeof AppLayer>
