import os

print("=== v1.31a: fix anime routing + wire Grid Columns setting ===")

# ── Helper: patch a file's ContentAdapter open-callback to route anime items
#    to AnimeDetailActivity instead of always going to WatchActivity. ──
def patch_open_callback(path, ctx_expr, is_inline_lambda):
    if not os.path.exists(path):
        print(f"[WARN] {path} not found — skipping")
        return
    content = open(path, encoding='utf-8').read()
    old = ('Intent i=new Intent(' + ctx_expr + ',WatchActivity.class);'
           'i.putExtra("type",item.mediaType);i.putExtra("id",item.id);startActivity(i);')
    new = (
        'if("anime".equals(item.mediaType)){'
        'Intent i=new Intent(' + ctx_expr + ',com.fountainhome.streaming.ui.AnimeDetailActivity.class);'
        'i.putExtra("anime_id",item.id);i.putExtra("title",item.displayTitle());'
        'i.putExtra("cover",item.posterPath);i.putExtra("banner",item.backdropPath);'
        'i.putExtra("rating",item.rating);startActivity(i);'
        '}else{'
        'Intent i=new Intent(' + ctx_expr + ',WatchActivity.class);'
        'i.putExtra("type",item.mediaType);i.putExtra("id",item.id);startActivity(i);'
        '}'
    )
    if old in content:
        content = content.replace(old, new, 1)
        open(path, 'w', encoding='utf-8').write(content)
        print(f"[OK] {os.path.basename(path)} — anime items now route to AnimeDetailActivity")
    elif 'AnimeDetailActivity.class);' in content and 'anime_id' in content:
        print(f"[SKIP] {os.path.basename(path)} already patched")
    else:
        print(f"[WARN] Could not find the expected open-callback in {os.path.basename(path)}.")
        print("       Wrap its WatchActivity Intent in an if(\"anime\".equals(item.mediaType)) branch manually.")

patch_open_callback("app/src/main/java/com/fountainhome/streaming/ui/fragment/HomeFragment.java", "getContext()", False)
patch_open_callback("app/src/main/java/com/fountainhome/streaming/ui/fragment/WatchListFragment.java", "getContext()", True)
patch_open_callback("app/src/main/java/com/fountainhome/streaming/ui/fragment/HistoryFragment.java", "getContext()", True)
patch_open_callback("app/src/main/java/com/fountainhome/streaming/ui/SearchActivity.java", "this", True)

# ── Wire the (previously stored-but-unused) grid columns preference into every
#    GridLayoutManager, instead of the hardcoded 3. ──
def patch_grid_columns(path):
    if not os.path.exists(path):
        print(f"[WARN] {path} not found — skipping")
        return
    content = open(path, encoding='utf-8').read()
    changed = False
    for ctx_call in ["getContext()", "this"]:
        old = f"new GridLayoutManager({ctx_call},3)"
        new = f"new GridLayoutManager({ctx_call},com.fountainhome.streaming.service.AppPreferences.getGridColumns({ctx_call}))"
        if old in content:
            content = content.replace(old, new)
            changed = True
    if changed:
        open(path, 'w', encoding='utf-8').write(content)
        print(f"[OK] {os.path.basename(path)} — grid now honors the Grid Columns setting")
    elif "getGridColumns" in content:
        print(f"[SKIP] {os.path.basename(path)} already wired")
    else:
        print(f"[WARN] Could not find a plain GridLayoutManager(...,3) call in {os.path.basename(path)}.")

for f in [
    "app/src/main/java/com/fountainhome/streaming/ui/fragment/MoviesFragment.java",
    "app/src/main/java/com/fountainhome/streaming/ui/fragment/TVFragment.java",
    "app/src/main/java/com/fountainhome/streaming/ui/fragment/WatchListFragment.java",
    "app/src/main/java/com/fountainhome/streaming/ui/fragment/HistoryFragment.java",
    "app/src/main/java/com/fountainhome/streaming/ui/SearchActivity.java",
]:
    patch_grid_columns(f)

print()
print("=== v1.31a complete ===")
