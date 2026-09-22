export interface HttpFailureEvent {
  kind: 'response' | 'network'
  error: Error
  cause?: unknown
  status?: number
}

export type HttpFailureHandler = (event: HttpFailureEvent) => void

let failureHandler: HttpFailureHandler | undefined

export function configureHttpFailureHandler(handler?: HttpFailureHandler): void {
  failureHandler = handler
}

export function reportHttpFailure(event: HttpFailureEvent): void {
  failureHandler?.(event)
}
