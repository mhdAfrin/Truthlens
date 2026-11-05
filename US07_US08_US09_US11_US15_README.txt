TRUTHLENS - SPRINT 2 VERIFICATION

Run npm install and npm run dev from the project folder. The local preview
uses H2. See README.md for the Java, Maven and Node.js requirements.

US-07: The report saves each processing stage before the next stage starts.
Search failures preserve partial findings. Sprint 1 requests prepared by an
earlier version can be opened from history and submitted again for assessment.

US-08: The report lists sentence-level potential claims with the original
passages and offsets. Heuristics identify questions, first-person opinions
and predictions, and retain factual statements about reported announcements.

US-09: Wikipedia candidates can have titles different from the claim. The
report shows exact retrieved passages and links behind supported and
contradicted findings, alongside a separate section for contextual sources.

US-11: Each source has a clickable page link, title, publisher and date
when available. Wikipedia results are deduplicated by page ID. The original
source's role is Unknown where it cannot be established, and Wikipedia pages
are not counted as independent corroboration.

US-15: The verifier checks direct matching statements and founding-year
claims with matching entities and events. Conflicts are labeled Mixed
evidence. Other claims with no clear match receive Insufficient evidence.
Search failures are labeled Not assessed. The current rules do not assess
arbitrary paraphrases or establish independent corroboration.
