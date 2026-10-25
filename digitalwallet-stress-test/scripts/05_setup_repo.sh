# Chuẩn bị repo SUT: clone từ GitHub HOẶC copy từ Kaggle input.
#
# Env:
#   REPO_SOURCE=github|kaggle|auto   (default: auto)
#   REPO_URL=<git url>               (github mode)
#   REPO_BRANCH=<branch>             (github mode, default = remote HEAD)
#   KAGGLE_DATASET_PATH=<path>       (kaggle mode)
#   REPO_DIR=<path>                  (default: /tmp/SAUETDigitalWallet)
#
# Output: ghi REPO_DIR vào $SUT_DIR_FILE
set -euo pipefail
source "$(dirname "$0")/_common.sh"
trap_errors

: "${REPO_SOURCE:=auto}"
: "${REPO_URL:=$SUT_REPO}"
: "${REPO_BRANCH:=}"
: "${KAGGLE_DATASET_PATH:=}"
: "${REPO_DIR:=$SUT_DIR}"

if [ "$REPO_SOURCE" = "auto" ]; then
  if [ -n "$KAGGLE_DATASET_PATH" ] && [ -d "$KAGGLE_DATASET_PATH" ]; then
    REPO_SOURCE=kaggle
  else
    REPO_SOURCE=github
  fi
fi
log "REPO_SOURCE=$REPO_SOURCE, REPO_DIR=$REPO_DIR"

validate_repo() {
  local dir="$1"
  for d in digitalwallet-docs digitalwallet-system digitalwallet-stress-test; do
    [ -d "$dir/$d" ] || die "Repo không đầy đủ: thiếu $dir/$d"
  done
}

case "$REPO_SOURCE" in
  github)
    need git
    MARKER=/tmp/repo_state_github.txt
    CUR=$(printf '%s\n%s' "$REPO_URL" "$REPO_BRANCH")
    PREV=""; [ -f "$MARKER" ] && PREV=$(cat "$MARKER")

    if [ -d "$REPO_DIR/.git" ] && [ "$CUR" = "$PREV" ]; then
      log "Cùng URL+branch → fetch cập nhật"
      git -C "$REPO_DIR" fetch --depth 1 origin
      if [ -n "$REPO_BRANCH" ]; then
        REF="origin/$REPO_BRANCH"
      else
        REF=$(git -C "$REPO_DIR" rev-parse origin/HEAD 2>/dev/null \
              || git -C "$REPO_DIR" rev-parse origin/main 2>/dev/null \
              || git -C "$REPO_DIR" rev-parse origin/master)
      fi
      git -C "$REPO_DIR" reset --hard "$REF"
    else
      log "Clone mới: $REPO_URL branch=${REPO_BRANCH:-<default>}"
      rm -rf "$REPO_DIR"
      if [ -n "$REPO_BRANCH" ]; then
        git clone --depth 1 --branch "$REPO_BRANCH" "$REPO_URL" "$REPO_DIR"
      else
        git clone --depth 1 "$REPO_URL" "$REPO_DIR"
      fi
      printf '%s\n%s' "$REPO_URL" "$REPO_BRANCH" > "$MARKER"
    fi
    git -C "$REPO_DIR" log -1 --oneline
    ;;

  kaggle)
    [ -n "$KAGGLE_DATASET_PATH" ] || die "KAGGLE_DATASET_PATH bắt buộc khi REPO_SOURCE=kaggle"
    [ -d "$KAGGLE_DATASET_PATH" ] || die "KAGGLE_DATASET_PATH không tồn tại: $KAGGLE_DATASET_PATH"
    MARKER=/tmp/repo_state_kaggle.txt
    CUR=$(realpath "$KAGGLE_DATASET_PATH")
    PREV=""; [ -f "$MARKER" ] && PREV=$(cat "$MARKER")

    if [ -d "$REPO_DIR" ] && [ "$CUR" = "$PREV" ]; then
      log "Cùng dataset path → dùng lại"
    else
      log "Copy dataset → $REPO_DIR"
      rm -rf "$REPO_DIR"
      cp -r "$KAGGLE_DATASET_PATH" "$REPO_DIR"
      printf '%s' "$CUR" > "$MARKER"
    fi
    ;;

  *) die "REPO_SOURCE không hợp lệ: $REPO_SOURCE (github|kaggle|auto)" ;;
esac

# Xử lý dataset có 1 wrapper dir.
if [ ! -d "$REPO_DIR/digitalwallet-system" ]; then
  for candidate in "$REPO_DIR"/*/; do
    if [ -d "${candidate}digitalwallet-system" ]; then
      log "Phát hiện wrapper dir: $candidate"
      REPO_DIR="${candidate%/}"
      break
    fi
  done
fi

validate_repo "$REPO_DIR"
mkdir -p "$(dirname "$SUT_DIR_FILE")"
echo "$REPO_DIR" > "$SUT_DIR_FILE"
log "OK → $SUT_DIR_FILE = $REPO_DIR"