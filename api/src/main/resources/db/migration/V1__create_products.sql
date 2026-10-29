CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name TEXT NOT NULL,
    -- Names are stored normalized: uppercase, never empty, no leading or trailing spaces. The API
    -- trims and uppercases before saving.
    CONSTRAINT products_name_normalized CHECK (name <> '' AND name = upper(btrim(name))),
    CONSTRAINT products_name_max_length CHECK (char_length(name) <= 50)
)
