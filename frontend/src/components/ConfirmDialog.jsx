import ModalDialog from './ModalDialog';

export default function ConfirmDialog({ id, title, description, onConfirm, confirmLabel = 'Confirm' }) {
  return (
    <ModalDialog
      id={id}
      title={title}
      footer={(
        <>
          <button className="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
          <button className="btn btn-danger" data-bs-dismiss="modal" onClick={onConfirm}>{confirmLabel}</button>
        </>
      )}
    >
      <p className="mb-0">{description}</p>
    </ModalDialog>
  );
}
