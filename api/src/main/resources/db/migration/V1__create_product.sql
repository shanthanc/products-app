CREATE TABLE product (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name TEXT NOT NULL check(btrim(name, E' \t\r\n')<> '')
)
