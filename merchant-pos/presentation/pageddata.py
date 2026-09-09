import math


class PagedData:
    def __init__(self) -> None:
        self.data_pages: dict[int, bytes] = {}
        self.max_pages = 1 << 64

    def get_data_pages_from_data(self, data: bytes, page_size: int) -> list[bytes]:
        # Data is divided into one or more pages, and a header is added to each page.
        # - The first byte stores the meta info:
        #   - bit 7 indicates that the page is the final page in sequence.
        #   - bits 6:3 are reserved.
        #   - bits 2:0 indicates the number of bytes required to store the page
        #     number, minus 1.
        # - The next byte(s) store the variable-width page number.

        page = 0
        data_offset = 0

        assert page_size > 5  # Header (1 byte) + Page Number (max 4 bytes)

        while data_offset < len(data):
            header_meta = 0

            header_meta_page_number_bytes = max(math.ceil(math.log2(page + 1) / 8), 1)
            assert header_meta_page_number_bytes <= 8
            header_meta |= header_meta_page_number_bytes - 1

            header_page_number = page.to_bytes(header_meta_page_number_bytes, "little")

            header_len = 1 + header_meta_page_number_bytes
            data_len = page_size - header_len

            page_data = data[data_offset : data_offset + data_len]

            data_offset += data_len
            if data_offset >= len(data):
                header_meta |= 1 << 7  # last page

            self.data_pages[page] = (
                header_meta.to_bytes(1) + header_page_number + page_data
            )

            page += 1

        return list(self.data_pages.values())
