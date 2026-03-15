import { describe, it, expect } from 'vitest'
import { parseSseEvent } from '../../src/protocol/parse'

describe('parseSseEvent', () => {
  it('parses workflow_plan event', () => {
    const json = JSON.stringify({
      type: 'workflow_plan',
      steps: ['human_requirements', 'weird_problem_generation'],
    })
    const result = parseSseEvent(json)
    expect(result.type).toBe('workflow_plan')
    if (result.type === 'workflow_plan') {
      expect(result.steps).toEqual(['human_requirements', 'weird_problem_generation'])
    }
  })

  it('parses message event with null source_node', () => {
    const json = JSON.stringify({
      type: 'message',
      content: 'Hello world',
      format: 'text',
      source_node: null,
    })
    const result = parseSseEvent(json)
    expect(result.type).toBe('message')
    if (result.type === 'message') {
      expect(result.content).toBe('Hello world')
      expect(result.source_node).toBeNull()
    }
  })

  it('parses message event with source_node', () => {
    const json = JSON.stringify({
      type: 'message',
      content: 'Step output',
      format: 'text',
      source_node: 'trend_analysis',
    })
    const result = parseSseEvent(json)
    expect(result.type).toBe('message')
    if (result.type === 'message') {
      expect(result.source_node).toBe('trend_analysis')
    }
  })

  it('parses input_request event with options', () => {
    const json = JSON.stringify({
      type: 'input_request',
      prompt: 'Choose one',
      options: ['Option A', 'Option B'],
      source_node: null,
    })
    const result = parseSseEvent(json)
    expect(result.type).toBe('input_request')
    if (result.type === 'input_request') {
      expect(result.prompt).toBe('Choose one')
      expect(result.options).toEqual(['Option A', 'Option B'])
    }
  })

  it('parses node_complete event', () => {
    const json = JSON.stringify({
      type: 'node_complete',
      node: 'trend_analysis',
    })
    const result = parseSseEvent(json)
    expect(result.type).toBe('node_complete')
    if (result.type === 'node_complete') {
      expect(result.node).toBe('trend_analysis')
    }
  })

  it('parses workflow_complete event', () => {
    const json = JSON.stringify({
      type: 'workflow_complete',
      chat_id: 'abc-123',
    })
    const result = parseSseEvent(json)
    expect(result.type).toBe('workflow_complete')
    if (result.type === 'workflow_complete') {
      expect(result.chat_id).toBe('abc-123')
    }
  })

  it('parses error event', () => {
    const json = JSON.stringify({ type: 'error', message: 'Something went wrong' })
    const result = parseSseEvent(json)
    expect(result.type).toBe('error')
    if (result.type === 'error') {
      expect(result.message).toBe('Something went wrong')
    }
  })

  it('throws on unknown event type', () => {
    const json = JSON.stringify({ type: 'unknown_type', data: 'x' })
    expect(() => parseSseEvent(json)).toThrow()
  })

  it('throws on invalid JSON', () => {
    expect(() => parseSseEvent('not-json')).toThrow()
  })
})
