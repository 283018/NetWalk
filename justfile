# pokaż opcje
default:
    @echo "Global commands:"
    @just --list

# zainstaluj hooki dla całego repo
install-hooks:
    pre-commit install

# uruchom wszystkie hooki ręcznie
run-hooks:
    pre-commit run --all-files

# zaktualizuj wersje hooków
update-hooks:
    pre-commit autoupdate

# odpal komendy z subfolderu backend
mod backend
# odpal komendy z subfolderu frontend
mod frontend
# odpal komendy z subfolderu androidApp
mod androidApp
