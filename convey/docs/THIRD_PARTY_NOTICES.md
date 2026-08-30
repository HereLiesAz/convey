# Third-party data notices

`internal/ConveyVerbData.kt` (generated; see `kinetic-text-verb-classification.md`'s
**Generation pipeline** section) embeds data derived from two external lexical resources.
`internal/ConveyNounData.kt` (generated; see the "Implementation status" section of
`Procedural Animation of Subject-Verb-Object Typography.md`) embeds data derived from the same
WordNet distribution's noun files (`index.noun`, `data.noun`, `noun.exc`) — the license below
covers it as well; no separate license text is needed since it is the same license, same source
distribution, same terms.

## Princeton WordNet 3.0

`ConveyVerbData.kt` embeds WordNet's verb lemma index, synset domains, and synset glosses
(definition text) in full, as permitted by WordNet's license; `ConveyNounData.kt` embeds the
equivalent noun lemma index, synset domains, and synset glosses in full, under the same license,
reproduced here per that license's own requirement that this notice "appear on ALL copies of the
software, database and documentation, including modifications":

> This software and database is being provided to you, the LICENSEE, by Princeton University
> under the following license. By obtaining, using and/or copying this software and database,
> you agree that you have read, understood, and will comply with these terms and conditions.:
>
> Permission to use, copy, modify and distribute this software and database and its
> documentation for any purpose and without fee or royalty is hereby granted, provided that you
> agree to comply with the following copyright notice and statements, including the disclaimer,
> and that the same appear on ALL copies of the software, database and documentation, including
> modifications that you make for internal use or for distribution.
>
> WordNet 3.0 Copyright 2006 by Princeton University. All rights reserved.
>
> THIS SOFTWARE AND DATABASE IS PROVIDED "AS IS" AND PRINCETON UNIVERSITY MAKES NO
> REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED. BY WAY OF EXAMPLE, BUT NOT LIMITATION,
> PRINCETON UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES OF MERCHANTABILITY OR FITNESS FOR
> ANY PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE, DATABASE OR DOCUMENTATION
> WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER RIGHTS.
>
> The name of Princeton University or Princeton may not be used in advertising or publicity
> pertaining to distribution of the software and/or database. Title to copyright in this
> software, database and any associated documentation shall at all times remain with Princeton
> University and LICENSEE agrees to preserve same.

Source: `https://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz`, via the NLTK data mirror at
`https://raw.githubusercontent.com/nltk/nltk_data/gh-pages/packages/corpora/wordnet.zip`.

## VerbNet 3.3

`ConveyVerbData.kt` does **not** embed VerbNet's XML text, examples, or documentation — only a
computed byproduct (a WordNet synset offset → `ConveyVerbClass` code table, produced by analyzing
each class's member sense-keys and `SEMANTICS`/`PRED` predicates at generation time; see the
pipeline description in `kinetic-text-verb-classification.md`). VerbNet itself is used only as a
build-time input, per its listed distribution terms ("Distributed with permission of the author",
Karin Kipper-Schuler).

Source: `https://verbs.colorado.edu/verbnet/`, via the NLTK data mirror at
`https://raw.githubusercontent.com/nltk/nltk_data/gh-pages/packages/corpora/verbnet3.zip`.
