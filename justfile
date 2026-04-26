# pokaż opcje
default:
    @echo "Commands:"
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
