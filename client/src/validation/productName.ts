// Fast feedback in the UI only; the API is the source of truth for these rules.

const MAX_NAME_LENGTH = 50

// Same blank check as the API's @NotBlank (Java's Character.isWhitespace): whitespace is tab to
// carriage return, U+001C to U+001F, and Unicode separators (\p{Z}) except the no-break spaces
// U+00A0, U+2007 and U+202F, which count as characters.
// oxlint-disable-next-line no-control-regex -- U+001C to U+001F are intended: the API treats them as whitespace
const NON_BLANK_CHARACTER = /[^\p{Z}\t\n\v\f\r\u001C-\u001F]|[\u00A0\u2007\u202F]/u

// Same as the API's @Pattern: after optional leading spaces, a letter, then only letters, digits, spaces and - & _.
const ALLOWED_NAME = /^ *([A-Za-z][A-Za-z0-9 &_-]*)?$/

// Returns the problem with the name, or null when the API should be asked.
export function validateProductName(name: string): string | null {
  if (!NON_BLANK_CHARACTER.test(name)) {
    return 'Name is required'
  }
  if (!ALLOWED_NAME.test(name)) {
    return 'Name must start with a letter and contain only letters, numbers, spaces and - & _'
  }
  if (name.length > MAX_NAME_LENGTH) {
    return `Name must not exceed ${MAX_NAME_LENGTH} characters`
  }
  return null
}
