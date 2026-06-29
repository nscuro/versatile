MVN := $(shell command -v mvn 2>/dev/null)
MVND := $(shell command -v mvnd 2>/dev/null)
ifeq ($(MVND),)
	MVND := $(MVN)
endif

clean:
	@$(MVND) -B -q clean
.PHONY: clean

lint:
	@$(MVND) -B -q spotless:check
.PHONY: lint

format:
	@$(MVND) -B -q spotless:apply
.PHONY: format

test:
	@$(MVND) -B verify
.PHONY: test

install:
	@$(MVND) -B install -DskipTests
.PHONY: install