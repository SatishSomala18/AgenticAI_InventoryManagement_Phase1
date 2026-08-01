import { useEffect, useMemo, useState } from 'react';
import Pagination from './Pagination';
import EmptyState from './EmptyState';

export default function DataTable({
  columns,
  data,
  pageSize = 10,
  rowActions,
  caption = 'Data table',
}) {
  const [page, setPage] = useState(1);

  const totalPages = Math.max(1, Math.ceil(data.length / pageSize));
  const currentPage = Math.min(page, totalPages);

  useEffect(() => {
    setPage(1);
  }, [data.length, pageSize]);

  const pagedData = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return data.slice(start, start + pageSize);
  }, [data, currentPage, pageSize]);

  if (!data.length) {
    return <EmptyState />;
  }

  return (
    <div className="card border-0 shadow-sm data-table-shell">
      <table
        className="table table-hover align-middle mb-0 app-data-table"
        role="table"
      >
        <caption className="visually-hidden">
          {caption}
        </caption>

        <thead className="table-light">
          <tr>
            {columns.map((column) => (
              <th key={column.key} scope="col">
                {column.title}
              </th>
            ))}
            {rowActions ? <th scope="col">Actions</th> : null}
          </tr>
        </thead>

        <tbody>
          {pagedData.map((row, index) => (
            <tr key={row.id || index} className="data-row">
              {columns.map((column) => (
                <td key={column.key}>
                  {column.render
                    ? column.render(row)
                    : row[column.key]}
                </td>
              ))}

              {rowActions ? (
                <td className="actions-cell">
                  {rowActions(row)}
                </td>
              ) : null}
            </tr>
          ))}
        </tbody>
      </table>

      <div className="card-footer bg-transparent border-0 d-flex justify-content-end">
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      </div>
    </div>
  );
}