package sding.protocol

import chat4s.ai.LlmProvider
import io.circe.Decoder
import io.circe.Encoder

final case class LlmConfigResponse(
    provider: LlmProvider,
    model: String,
    keyHint: String
) derives Decoder,
      Encoder.AsObject

final case class LlmConfigRequest(
    provider: LlmProvider,
    apiKey: String,
    model: String
) derives Decoder,
      Encoder.AsObject
