/* Avvio di Grabatron per Windows: esegue il Java incluso senza console. */
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <shellapi.h>
#include <wchar.h>

static void fail(const wchar_t *msg) {
    MessageBoxW(NULL, msg, L"Grabatron", MB_OK | MB_ICONERROR);
}

int WINAPI wWinMain(HINSTANCE hi, HINSTANCE hp, PWSTR args, int show) {
    wchar_t dir[MAX_PATH], javaw[MAX_PATH + 32], jar[MAX_PATH + 32], logp[MAX_PATH + 32];
    static wchar_t cmd[8192];
    DWORD n = GetModuleFileNameW(NULL, dir, MAX_PATH);
    if (n == 0 || n >= MAX_PATH) { fail(L"Percorso del programma non valido."); return 1; }
    wchar_t *slash = wcsrchr(dir, L'\\');
    if (slash) *slash = 0;
    SetCurrentDirectoryW(dir);

    swprintf(javaw, MAX_PATH + 32, L"%ls\\jre\\bin\\javaw.exe", dir);
    swprintf(jar, MAX_PATH + 32, L"%ls\\game.jar", dir);
    swprintf(logp, MAX_PATH + 32, L"%ls\\grabatron.log", dir);
    if (GetFileAttributesW(javaw) == INVALID_FILE_ATTRIBUTES) {
        fail(L"Manca la cartella 'jre'.\nEstrai TUTTO lo zip e avvia Grabatron.exe dalla cartella estratta.");
        return 1;
    }
    if (GetFileAttributesW(jar) == INVALID_FILE_ATTRIBUTES) {
        fail(L"Manca game.jar.\nEstrai TUTTO lo zip e avvia Grabatron.exe dalla cartella estratta.");
        return 1;
    }
    swprintf(cmd, 8192, L"\"%ls\" -Xms256m -Xmx1g -Dfile.encoding=UTF-8 -jar \"%ls\" %ls", javaw, jar, args ? args : L"");

    SECURITY_ATTRIBUTES sa = { sizeof(sa), NULL, TRUE };
    HANDLE log = CreateFileW(logp, GENERIC_WRITE, FILE_SHARE_READ, &sa, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    STARTUPINFOW si; PROCESS_INFORMATION pi;
    ZeroMemory(&si, sizeof(si)); si.cb = sizeof(si);
    if (log != INVALID_HANDLE_VALUE) {
        si.dwFlags = STARTF_USESTDHANDLES;
        si.hStdOutput = log; si.hStdError = log; si.hStdInput = NULL;
    }
    ZeroMemory(&pi, sizeof(pi));
    if (!CreateProcessW(javaw, cmd, NULL, NULL, TRUE, 0, NULL, dir, &si, &pi)) {
        fail(L"Impossibile avviare Java (jre\\bin\\javaw.exe).");
        return 1;
    }
    WaitForSingleObject(pi.hProcess, INFINITE);
    DWORD code = 0;
    GetExitCodeProcess(pi.hProcess, &code);
    CloseHandle(pi.hProcess); CloseHandle(pi.hThread);
    if (log != INVALID_HANDLE_VALUE) CloseHandle(log);
    if (code != 0) {
        fail(L"Grabatron si e' chiuso con un errore.\nI dettagli sono nel file grabatron.log nella cartella del gioco.");
    }
    return (int) code;
}
