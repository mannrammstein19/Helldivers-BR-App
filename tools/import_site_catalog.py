"""python tools/import_site_catalog.py /path/to/estratagemas.html
Imports source values as-is; no invented paths or stats. Python standard library only.
"""
import json
import sys
from html.parser import HTMLParser
from pathlib import Path

class Node:
    def __init__(self, tag='', attrs=()):
        self.tag, self.attrs, self.children = tag, dict(attrs), []
    def all(self, tag=None, cls=None):
        found = []
        for child in self.children:
            if isinstance(child, Node):
                if (tag is None or child.tag == tag) and (cls is None or cls in child.attrs.get('class','').split()):
                    found.append(child)
                found.extend(child.all(tag, cls))
        return found
    def text(self):
        return ' '.join(' '.join(c.text() if isinstance(c,Node) else c for c in self.children).split())

class Parser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.root=Node(); self.stack=[self.root]
    def handle_starttag(self, tag, attrs):
        node=Node(tag,attrs); self.stack[-1].children.append(node)
        if tag not in {'area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr'}:
            self.stack.append(node)
    def handle_endtag(self, tag):
        for i in range(len(self.stack)-1,0,-1):
            if self.stack[i].tag==tag:
                del self.stack[i:]; break
    def handle_data(self, data): self.stack[-1].children.append(data)

def extract(path):
    parser=Parser();parser.feed(Path(path).read_text(encoding='utf-8'))
    result=[]
    arrows={'seta-cima.png':'↑','seta-baixo.png':'↓','seta-esquerda.png':'←','seta-direita.png':'→'}
    for group in parser.root.all(cls='stratagem-group'):
        permission=group.all(cls='group-title')[0].text()
        for category in group.all(cls='accordion-category'):
            title=category.all(cls='category-title')[0].text().split('(')[0].strip()
            for row in category.all('tr'):
                cells=row.all('td')
                if not cells: continue
                if len(cells)!=7: raise ValueError(f'Unexpected columns: {title}: {len(cells)}')
                images=cells[0].all('img'); links=cells[1].all('a')
                code=' '.join(arrows[n.attrs['src'].split('/')[-1]] for n in cells[2].all('img', 'arrow')) or cells[2].text()
                result.append(dict(name=cells[1].text(),category=title,permission=permission,
                    icon=images[0].attrs.get('src','') if images else '',
                    path=links[0].attrs.get('href','') if links else '',code=code,
                    cooldown=cells[3].text(),cost=cells[4].text(),level=cells[5].text(),source=cells[6].text()))
    if not result: raise ValueError('No catalog rows found')
    return result

if __name__=='__main__':
    entries=extract(sys.argv[1])
    out=Path(__file__).resolve().parents[1]/'app/src/main/assets/stratagems.json'
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(entries,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    from collections import Counter
    print(f'{len(entries)} registros: {dict(Counter(e["category"] for e in entries))}')
