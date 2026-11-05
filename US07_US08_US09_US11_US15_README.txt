TRUTHLENS - INTEGRATED FIVE-STORY PROTOTYPE

Run on Windows: open the TruthLens folder in IntelliJ, run the root npm script "dev" as before.
Prerequisites: Node.js, Java 17+, Maven. The root dev script starts the Spring Boot preview backend with its H2 test database and Vite frontend.

US-07: Queued -> Processing (claim extraction / evidence search) -> Completed, or Failed. View the stage, open stored reports and partial findings on failure.
US-08: Sentence-based potential-claim extraction with unique per-report IDs, original passages and offsets, and a basic opinion/prediction heuristic. This is not a machine-learning claim classifier.
US-09: Attempts live public-source discovery via Wikipedia's search API, distinguishes RESULTS_FOUND, NO_RESULTS, SEARCH_FAILED, and NOT_APPLICABLE. Internet access is required. Search results are leads, not independently verified evidence.
US-11: Displays retrieved source titles, URLs, excerpts, source types, publishers and dates (when available); deduplicates by URL. A source link can be opened in a browser.
US-15: Every claim has an explicit finding and explanation. Unverified factual claims receive Insufficient evidence, while subjective sentences receive Not assessed. This prototype does NOT automatically conclude Supported or Contradicted, because source-search results cannot justify those verdicts.

IMPORTANT: The prototype is NOT a production fact-checking system. It does not establish factual truth, assess independent corroboration, or guarantee external evidence availability. Completed means the pipeline finished, not that all claims were verified.

Validation in this environment: TypeScript typecheck passed. Backend compilation, startup and end-to-end behavior were not executed because Maven and a runnable database were unavailable here. Please do not describe this as end-to-end tested.
