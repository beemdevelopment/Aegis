# pandoc format parameters taken from https://github.com/TokTok/spec/blob/54ee53de924210295e311d10c63527c47468305c/pandoc.mk
FORMAT := markdown_github
FORMAT := $(FORMAT)-hard_line_breaks
FORMAT := $(FORMAT)-native_spans
FORMAT := $(FORMAT)-raw_html
FORMAT := $(FORMAT)+tex_math_dollars
FORMAT := $(FORMAT)-tex_math_single_backslash

PANDOC_ARGS :=			\
	--no-tex-ligatures	\
	--atx-headers		\
	--columns=79		\

all:
	printf "nope"

format:
	pandoc $(PANDOC_ARGS) -f $(FORMAT) -t $(FORMAT) doc/db.md -o doc/db.md