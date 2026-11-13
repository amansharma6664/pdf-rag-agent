import { useRef, useState } from "react";
import { Check, FileText, KeyRound, Loader2, Trash2, UploadCloud, X } from "lucide-react";

export default function Sidebar({
  docs,
  selected,
  uploading,
  status,
  open,
  onClose,
  onUpload,
  onDelete,
  onToggle,
}) {
  const inputRef = useRef(null);
  const [dragging, setDragging] = useState(false);

  const handleDrop = (e) => {
    e.preventDefault();
    setDragging(false);
    if (e.dataTransfer.files.length > 0) onUpload(e.dataTransfer.files);
  };

  const handleBrowse = (e) => {
    if (e.target.files.length > 0) onUpload(e.target.files);
    e.target.value = "";
  };

  return (
    <aside className={`sidebar ${open ? "is-open" : ""}`}>
      <div className="sidebar-top">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            <span className="brand-mark-line" />
          </span>
          <div>
            <h1>Pagewise</h1>
            <p>Answers from your PDFs, with page numbers</p>
          </div>
        </div>
        <button className="icon-btn drawer-close" onClick={onClose} aria-label="Close documents">
          <X size={20} />
        </button>
      </div>

      {status && !status.openaiConfigured && (
        <div className="key-warning" role="status">
          <KeyRound size={18} />
          <p>
            OpenAI API key is missing. Add <code>OPENAI_API_KEY</code> to the <code>.env</code> file and
            restart the app.
          </p>
        </div>
      )}

      <div
        className={`dropzone ${dragging ? "is-dragging" : ""}`}
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
        onClick={() => inputRef.current?.click()}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            inputRef.current?.click();
          }
        }}
        role="button"
        tabIndex={0}
        aria-label="Upload PDF files"
      >
        <UploadCloud size={28} />
        <strong>{dragging ? "Drop to upload" : "Drop PDFs here"}</strong>
        <span>or click to choose files</span>
        <input
          ref={inputRef}
          type="file"
          accept="application/pdf,.pdf"
          multiple
          hidden
          onChange={handleBrowse}
        />
      </div>

      <div className="library">
        <h2>Your documents</h2>

        {uploading.map((name) => (
          <div className="doc doc-pending" key={`up-${name}`}>
            <Loader2 size={18} className="spin" />
            <div className="doc-text">
              <span className="doc-name">{name}</span>
              <span className="doc-meta">Reading and indexing…</span>
            </div>
          </div>
        ))}

        {docs.length === 0 && uploading.length === 0 && (
          <p className="library-empty">No PDFs yet. Upload one to start asking questions.</p>
        )}

        <ul className="doc-list">
          {docs.map((doc) => {
            const isOn = selected.has(doc.id);
            return (
              <li className={`doc ${isOn ? "is-on" : ""}`} key={doc.id}>
                <button
                  className="check"
                  role="checkbox"
                  aria-checked={isOn}
                  aria-label={`Search in ${doc.fileName}`}
                  onClick={() => onToggle(doc.id)}
                >
                  {isOn ? <Check size={14} strokeWidth={3} /> : <FileText size={14} />}
                </button>
                <div className="doc-text">
                  <span className="doc-name" title={doc.fileName}>
                    {doc.fileName}
                  </span>
                  <span className="doc-meta">
                    {doc.pageCount} {doc.pageCount === 1 ? "page" : "pages"} · {doc.chunkCount} passages
                  </span>
                </div>
                <button
                  className="icon-btn danger"
                  onClick={() => onDelete(doc.id)}
                  aria-label={`Delete ${doc.fileName}`}
                >
                  <Trash2 size={16} />
                </button>
              </li>
            );
          })}
        </ul>
      </div>
    </aside>
  );
}
