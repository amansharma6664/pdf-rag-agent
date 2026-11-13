import { useState } from "react";
import ReactMarkdown from "react-markdown";
import { AlertCircle, BookMarked } from "lucide-react";

function Sources({ sources }) {
  const [openIndex, setOpenIndex] = useState(null);
  if (!sources || sources.length === 0) return null;

  return (
    <div className="sources">
      <p className="sources-title">Found on these pages</p>
      <div className="tabs">
        {sources.map((s, i) => (
          <button
            key={`${s.documentId}-${s.page}-${i}`}
            className={`tab ${openIndex === i ? "is-open" : ""}`}
            onClick={() => setOpenIndex(openIndex === i ? null : i)}
            aria-expanded={openIndex === i}
            title={s.fileName}
          >
            <BookMarked size={14} />
            <span className="tab-file">{s.fileName}</span>
            <span className="tab-page">p. {s.page}</span>
          </button>
        ))}
      </div>
      {openIndex !== null && (
        <blockquote className="passage">
          <mark>{sources[openIndex].snippet}</mark>
          <footer>
            {sources[openIndex].fileName}, page {sources[openIndex].page} · match{" "}
            {Math.round(sources[openIndex].score * 100)}%
          </footer>
        </blockquote>
      )}
    </div>
  );
}

export default function Message({ message }) {
  if (message.role === "user") {
    return (
      <div className="msg msg-user">
        <p>{message.content}</p>
      </div>
    );
  }

  if (message.error) {
    return (
      <div className="msg msg-error" role="alert">
        <AlertCircle size={18} />
        <p>{message.content}</p>
      </div>
    );
  }

  return (
    <div className="msg msg-assistant">
      <div className="md">
        <ReactMarkdown>{message.content}</ReactMarkdown>
      </div>
      <Sources sources={message.sources} />
    </div>
  );
}
