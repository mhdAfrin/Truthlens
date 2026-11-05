export function Pager({
  page,
  total,
  size,
  setPage,
}: {
  page: number;
  total: number;
  size: number;
  setPage: (p: number) => void;
}) {
  return (
    <div className="admin-pagination">
      <button disabled={!page} onClick={() => setPage(page - 1)}>
        Previous
      </button>
      <span>
        Page {page + 1} of {Math.max(1, Math.ceil(total / size))}
      </span>
      <button disabled={(page + 1) * size >= total} onClick={() => setPage(page + 1)}>
        Next
      </button>
    </div>
  );
}
