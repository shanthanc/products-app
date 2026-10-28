// Fast feedback in the UI only; the API is the source of truth for these rules.

const MAX_NAME_LENGTH = 50

// Same blank check as the API: a name needs one character that is neither whitespace (tab to
// carriage return, U+001C to U+001F) nor a Unicode space separator (\p{Z}, e.g. the no-break space).
// oxlint-disable-next-line no-control-regex -- U+001C to U+001F are intended: the API treats them as whitespace
const NON_BLANK_CHARACTER = /[^\p{Z}\t\n\v\f\r\u001C-\u001F]/u

// Returns the problem with the name, or null when the API should be asked.
export function validateProductName(name: string): string | null {
  if (!NON_BLANK_CHARACTER.test(name)) {
    return 'Name is required'
  }
  if (name.length > MAX_NAME_LENGTH) {
    return `Name must be at most ${MAX_NAME_LENGTH} characters`
  }
  return null
}
