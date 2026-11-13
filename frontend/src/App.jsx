import { useCallback, useEffect, useRef, useState } from "react";
import { AlertCircle, X } from "lucide-react";
import Sidebar from "./components/Sidebar.jsx";
import Chat from "./components/Chat.jsx";
import { askQuestion, deleteDocument, getStatus, listDocuments, uploadDocument } from "./api.js";

export default function App() {
  const [docs, setDocs] = useState([]);
  const [selected, setSelected] = useState(new Set());
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState([]);
  const [status, setStatus] = useState(null);
  const [toast, setToast] = useState(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const nextId = useRef(1);

  const showError = useCallback((message) => setToast(message), []);

  // hide the error toast after a few seconds
  useEffect(() => {
    if (!toast) return undefined;
    const timer = setTimeout(() => setToast(null), 7000);
    return () => clearTimeout(timer);
  }, [toast]);

  // load status and the saved documents when the page opens
  useEffect(() => {
    getStatus().then(setStatus).catch((e) => showError(e.message));
    listDocuments()
      .then((list) => {
        setDocs(list);
        setSelected(new Set(list.map((d) => d.id)));
      })
      .catch((e) => showError(e.message));
  }, [showError]);

  const handleUpload = async (fileList) => {
    const files = Array.from(fileList).filter((f) => f.name.toLowerCase().endsWith(".pdf"));
    if (files.length === 0) {
      showError("Only PDF files are supported.");
      return;
    }
    for (const file of files) {
      setUploading((u) => [...u, file.name]);
      try {
        const doc = await uploadDocument(file);
        setDocs((d) => [doc, ...d]);
        setSelected((s) => new Set(s).add(doc.id));
      } catch (e) {
        showError(`${file.name}: ${e.message}`);
      } finally {
        setUploading((u) => u.filter((name) => name !== file.name));
      }
    }
  };

  const handleDelete = async (id) => {
    try {
      await deleteDocument(id);
      setDocs((d) => d.filter((doc) => doc.id !== id));
      setSelected((s) => {
        const next = new Set(s);
        next.delete(id);
        return next;
      });
    } catch (e) {
      showError(e.message);
    }
  };

  const handleToggle = (id) => {
    setSelected((s) => {
      const next = new Set(s);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleSend = async (question) => {
    const history = messages
      .filter((m) => !m.error)
      .slice(-6)
      .map((m) => ({ role: m.role, content: m.content }));

    setMessages((m) => [...m, { id: nextId.current++, role: "user", content: question }]);
    setLoading(true);
    try {
      const res = await askQuestion({ question, documentIds: [...selected], history });
      setMessages((m) => [
        ...m,
        { id: nextId.current++, role: "assistant", content: res.answer, sources: res.sources },
      ]);
    } catch (e) {
      setMessages((m) => [
        ...m,
        { id: nextId.current++, role: "assistant", content: e.message, error: true },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app">
      <Sidebar
        docs={docs}
        selected={selected}
        uploading={uploading}
        status={status}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onUpload={handleUpload}
        onDelete={handleDelete}
        onToggle={handleToggle}
      />
      {drawerOpen && <div className="scrim" onClick={() => setDrawerOpen(false)} />}

      <Chat
        messages={messages}
        loading={loading}
        totalDocs={docs.length}
        selectedCount={selected.size}
        onSend={handleSend}
        onClear={() => setMessages([])}
        onOpenSidebar={() => setDrawerOpen(true)}
      />

      {toast && (
        <div className="toast" role="alert">
          <AlertCircle size={18} />
          <span>{toast}</span>
          <button className="icon-btn" onClick={() => setToast(null)} aria-label="Dismiss message">
            <X size={16} />
          </button>
        </div>
      )}
    </div>
  );
}
