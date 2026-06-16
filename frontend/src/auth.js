let credentials = null;

export function setCredentials(username, password) {
  credentials = { username, password };
  localStorage.setItem("auth_user", username);
  localStorage.setItem("auth_pass", password);
}

export function getAuthHeader() {
  if (credentials) {
    return `Basic ${btoa(`${credentials.username}:${credentials.password}`)}`;
  }
  const storedUser = localStorage.getItem("auth_user");
  const storedPass = localStorage.getItem("auth_pass");
  if (storedUser && storedPass) {
    credentials = { username: storedUser, password: storedPass };
    return `Basic ${btoa(`${storedUser}:${storedPass}`)}`;
  }
  return null;
}

export async function authFetch(url, options = {}) {
  const header = getAuthHeader();
  if (!header) {
    const user = prompt("Nazwa użytkownika:");
    const pass = prompt("Hasło:");
    if (user && pass) {
      setCredentials(user, pass);
      return authFetch(url, options);
    } else {
      throw new Error("Brak autoryzacji");
    }
  }

  const response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      "Authorization": header,
      "Content-Type": "application/json",
    },
  });

  if (response.status === 401) {
    localStorage.removeItem("auth_user");
    localStorage.removeItem("auth_pass");
    credentials = null;
    return authFetch(url, options);
  }

  return response;
}