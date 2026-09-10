# Data Paging

## Purpose

Especially, when sending the catalog data to the application through some data
matrix code, the size of said encoding is limited to physical constraints. For
instance, if a QR code is used in order to encode it, data is only limited by
the standard and its two dimensions.

The latter approach, is what divides the data into multiple pages. Each page
has its designated maximum bytes by the standard, followed by some metadata
which allows identifying the page itself. These pages are displayed one after
other, sequentially in fixed time durations, thus employing time as the third
dimension.

## Design

### Header

#### Metadata

The first byte represents the header metadata. *(1 byte)*

- **Bits 0 through 2** specify the length of the page number. The length is
retrieved by the value in the bits, plus one.

- **Bits 4 through 6** are reserved and are to be explicitly zero.

- **Bit 7** represents the last page in sequence of pages.

#### Page Number

The page number is a 0-indexed number assigned to the chunk of data, denoting
its sequence. *(1 to 8 bytes, little-endian, variable)*
