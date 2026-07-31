import os

print("=== v1.32: fix HomeFragment build error (ObjectAnimator has no setRepeatDelay) ===")

path = "app/src/main/java/com/fountainhome/streaming/ui/fragment/HomeFragment.java"
if os.path.exists(path):
    content = open(path, encoding='utf-8').read()
    old = '''private void startTitleShimmer() {
        if (b == null) return;
        b.titleShimmer.post(() -> {
            if (b == null || b.appTitle.getWidth() == 0) return;
            float startX = -b.appTitle.getWidth() * 0.6f;
            float endX = b.appTitle.getWidth() * 1.2f;
            b.titleShimmer.setTranslationX(startX);
            ObjectAnimator anim = ObjectAnimator.ofFloat(b.titleShimmer, "translationX", startX, endX);
            anim.setDuration(1400);
            anim.setStartDelay(500);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setRepeatDelay(1800);
            anim.start();
        });
    }'''
    new = '''private void startTitleShimmer() {
        if (b == null) return;
        b.titleShimmer.post(this::runShimmerSweep);
    }
    private void runShimmerSweep() {
        if (b == null || b.appTitle.getWidth() == 0) return;
        float startX = -b.appTitle.getWidth() * 0.6f;
        float endX = b.appTitle.getWidth() * 1.2f;
        b.titleShimmer.setTranslationX(startX);
        ObjectAnimator anim = ObjectAnimator.ofFloat(b.titleShimmer, "translationX", startX, endX);
        anim.setDuration(1400);
        anim.setStartDelay(500);
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (b != null) b.titleShimmer.postDelayed(() -> runShimmerSweep(), 1800);
            }
        });
        anim.start();
    }'''
    if old in content:
        content = content.replace(old, new, 1)
        open(path, 'w', encoding='utf-8').write(content)
        print("[OK] HomeFragment.java — shimmer now uses a self-rescheduling loop instead of the nonexistent setRepeatDelay")
    elif "runShimmerSweep" in content:
        print("[SKIP] HomeFragment.java already patched")
    else:
        print("[WARN] Could not find the exact startTitleShimmer() method (whitespace may have drifted).")
        print("       Replace it with a version that uses AnimatorListenerAdapter.onAnimationEnd +")
        print("       postDelayed to reschedule the sweep, since ObjectAnimator has no setRepeatDelay().")
else:
    print("[WARN] HomeFragment.java not found — skipping")

print()
print("=== v1.32 complete ===")
