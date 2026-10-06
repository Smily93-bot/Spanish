"""Applies the reviewed glossary and Arabic example fixes to app/src/main/assets/vocab.json.

tools/glossary/*.tsv     rank<TAB>English gloss<TAB>Arabic gloss   (all 5000 frequency words)
tools/examples_ar/*.tsv  rank<TAB>corrected Arabic example        (applied to every word sharing that example)

Run from the repository root: python3 tools/apply_content.py
"""
import glob
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VOCAB = os.path.join(ROOT, "app/src/main/assets/vocab.json")

SENTENCE_OVERRIDES = {
    3569: ("Rellena esta ficha con tus datos.", "Fill in this form with your details.", "املأ هذه الاستمارة ببياناتك."),
}


def rows(pattern):
    for path in sorted(glob.glob(os.path.join(ROOT, pattern))):
        with open(path, encoding="utf-8") as f:
            for line in f:
                line = line.rstrip("\n")
                if line.strip():
                    yield path, line.split("\t")


def main():
    with open(VOCAB, encoding="utf-8") as f:
        vocab = json.load(f)
    words = {w["r"]: w for w in vocab["frequency"]}

    glossed = 0
    for path, f in rows("tools/glossary/*.tsv"):
        assert len(f) == 3, (path, f)
        w = words[int(f[0])]
        w["en"], w["ar"] = f[1].strip(), f[2].strip()
        glossed += 1

    # Examples replaced outright because the original sentence was unusable.
    for rank, (es, en, ar) in SENTENCE_OVERRIDES.items():
        words[rank].update(esx=es, enx=en, arx=ar)

    fixes = {}
    for path, f in rows("tools/examples_ar/*.tsv"):
        assert len(f) == 2, (path, f)
        fixes[words[int(f[0])]["esx"]] = f[1].strip()
    examples = 0
    for w in vocab["frequency"]:
        if w["esx"] in fixes:
            w["arx"] = fixes[w["esx"]]
            examples += 1

    with open(VOCAB, "w", encoding="utf-8") as f:
        json.dump(vocab, f, ensure_ascii=False, separators=(",", ":"))
    print(f"glosses: {glossed}, example sentences fixed: {len(fixes)} ({examples} entries)")


if __name__ == "__main__":
    main()
