MVN := $(shell command -v mvn 2>/dev/null)
MVND := $(shell command -v mvnd 2>/dev/null)
ifeq ($(MVND),)
	MVND := $(MVN)
endif

lint:
	@$(MVND) -B -q spotless:check
.PHONY: lint

format:
	@$(MVND) -B -q spotless:apply
.PHONY: format

test:
	@$(MVD) -B -q verify
.PHONY: test
