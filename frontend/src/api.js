// All calls to the Spring Boot backend live here.

async function request(url, options) {
  let res;
  try {
    res = await fetch(url, options);
  } catch {
    throw new Error("Can't reach the server. Check that the backend is running on port 8080.");
  }

  if (!res.ok) {
    let message = `Something went wrong (status ${res.status}).`;
    try {
      const data = await res.json();
      message = data.error || data.message || message;
    } catch {
      // response had no JSON body
    }
    throw new Error(message);
  }

  if (res.status === 204) return null;
  return res.json();
}

export const getStatus = () => request("/api/status");

export const listDocuments = () => request("/api/documents");

export const uploadDocument = (file) => {
  const form = new FormData();
  form.append("file", file);
  return request("/api/documents", { method: "POST", body: form });
};

export const deleteDocument = (id) => request(`/api/documents/${id}`, { method: "DELETE" });

export const askQuestion = (payload) =>
  request("/api/chat", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
