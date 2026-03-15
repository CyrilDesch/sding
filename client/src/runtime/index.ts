import { ManagedRuntime } from 'effect'
import { AppLayer, type AppServices } from './AppLayer'

export type AppRuntime = ManagedRuntime.ManagedRuntime<AppServices, never>

export const createAppRuntime = (): AppRuntime => ManagedRuntime.make(AppLayer)
